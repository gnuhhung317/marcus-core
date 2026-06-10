#!/usr/bin/env python3
import sys
import subprocess

def run_query(sql, tuples_only=True):
    """Run a query inside the Postgres docker container."""
    cmd = ["docker", "exec", "-i", "postgres", "psql", "-U", "user", "-d", "signal_db"]
    if tuples_only:
        cmd.extend(["-t", "-A"])
    cmd.extend(["-c", sql])
    
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error running database query: {result.stderr.strip()}", file=sys.stderr)
        sys.exit(1)
    return result.stdout.strip()

def check_postgres_running():
    cmd = ["docker", "inspect", "-f", "{{.State.Running}}", "postgres"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0 or "true" not in result.stdout.lower():
        print("Error: The 'postgres' docker container is not running.", file=sys.stderr)
        sys.exit(1)

def get_developers():
    """Fetch all unique developer_ids that have bots."""
    sql = "SELECT developer_id, COUNT(id) FROM bots GROUP BY developer_id;"
    output = run_query(sql)
    if not output:
        return []
    
    devs = []
    for line in output.split('\n'):
        parts = line.split('|')
        if len(parts) == 2:
            devs.append({"developer_id": parts[0], "bot_count": parts[1]})
    return devs

def get_bots_for_developer(developer_id):
    """Fetch all bots for a specific developer."""
    sql = f"SELECT id, bot_id, name, status FROM bots WHERE developer_id = '{developer_id}';"
    output = run_query(sql)
    if not output:
        return []
    
    bots = []
    for line in output.split('\n'):
        parts = line.split('|')
        if len(parts) >= 4:
            bots.append({
                "internal_id": parts[0],
                "bot_id": parts[1],
                "name": parts[2],
                "status": parts[3]
            })
    return bots

def delete_bot(internal_id, bot_id, name):
    """Delete a single bot and all its associated dependencies."""
    print(f"  -> Deleting bot '{name}' ({bot_id})...")
    sql_transaction = f"""
    BEGIN;
    DELETE FROM execution_event WHERE signal_id IN (SELECT signal_id FROM signals WHERE bot_id = '{bot_id}');
    DELETE FROM execution_state WHERE signal_id IN (SELECT signal_id FROM signals WHERE bot_id = '{bot_id}');
    DELETE FROM signals WHERE bot_id = '{bot_id}';
    DELETE FROM bot_asset_pairs WHERE bot_entity_id = '{internal_id}';
    DELETE FROM subscriptions WHERE bot_id = '{bot_id}';
    DELETE FROM raw_events WHERE bot_id = '{bot_id}';
    DELETE FROM bots WHERE id = '{internal_id}';
    COMMIT;
    """
    run_query(sql_transaction, tuples_only=False)
    print(f"     ✅ Cleaned up '{name}'.")

def main():
    check_postgres_running()
    print("=== Marcus Trading - Interactive Bot Cleanup ===")
    
    # 1. Select Developer
    devs = get_developers()
    if not devs:
        print("No bots found in the database. Everything is clean!")
        sys.exit(0)
        
    print("\n--- Developers with Bots ---")
    for idx, dev in enumerate(devs):
        dev_id = dev['developer_id'] if dev['developer_id'] else "System/Unknown"
        print(f"[{idx + 1}] Developer ID: {dev_id} (Bots: {dev['bot_count']})")
    print("[0] Exit")
    
    try:
        dev_choice = int(input("\nSelect a Developer by number: ").strip())
        if dev_choice == 0:
            sys.exit(0)
        selected_dev = devs[dev_choice - 1]['developer_id']
    except (ValueError, IndexError):
        print("Invalid selection. Exiting.")
        sys.exit(1)
        
    # 2. Select Bots to Delete
    bots = get_bots_for_developer(selected_dev)
    if not bots:
        print("No bots found for this developer.")
        sys.exit(0)
        
    print(f"\n--- Bots for Developer: {selected_dev if selected_dev else 'System/Unknown'} ---")
    for idx, bot in enumerate(bots):
        print(f"[{idx + 1}] {bot['name']} (ID: {bot['bot_id']} | Status: {bot['status']})")
    print("[A] Delete ALL bots for this developer")
    print("[0] Exit")
    
    bot_choice = input("\nSelect a Bot number to delete, or 'A' to delete ALL: ").strip().upper()
    
    if bot_choice == '0':
        sys.exit(0)
        
    # 3. Execute Deletion
    if bot_choice == 'A':
        confirm = input(f"Are you sure you want to delete ALL {len(bots)} bots for this developer? [y/N]: ").strip().lower()
        if confirm in ['y', 'yes']:
            print("\nStarting batch deletion...")
            for bot in bots:
                delete_bot(bot['internal_id'], bot['bot_id'], bot['name'])
            print("\n🎉 All bots for this developer deleted successfully!")
        else:
            print("Action canceled.")
    else:
        try:
            # Handle single or comma-separated multiple selection
            choices = [int(x.strip()) for x in bot_choice.split(',')]
            for choice in choices:
                if 1 <= choice <= len(bots):
                    bot = bots[choice - 1]
                    confirm = input(f"Delete '{bot['name']}'? [y/N]: ").strip().lower()
                    if confirm in ['y', 'yes']:
                        delete_bot(bot['internal_id'], bot['bot_id'], bot['name'])
                else:
                    print(f"Invalid selection: {choice}")
            print("\n🎉 Selected bots deleted successfully!")
        except ValueError:
            print("Invalid input. Please enter a number, comma-separated numbers, or 'A'. Exiting.")

if __name__ == "__main__":
    main()
